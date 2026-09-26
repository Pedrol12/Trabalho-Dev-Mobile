//importacoes

require('dotenv').config();

const express = require('express');

const { Pool } = require('pg');

const cors = require('cors');

const app = express();

const PORT = 3000;

app.use(cors());
app.use(express.json());

//conecao banco

const pool = new Pool({

    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
});

pool.connect((err,client,release) =>{
    if(err){
        console.error('Erro ao conectar ao banco', err.message);
    }
    else{
        console.log('Conectado ao banco');
        release();
    }
});
//funcao q cria as tabelas se n existir
async function criarTabelas() {
    try{
        await pool.query(`
            CREATE TABLE IF NOT EXISTS sessao_inferencia(
            id_sessao TEXT PRIMARY KEY,
            data_hora TEXT,
            nome_modelo TEXT,
            tempo_execucao_ms INTEGER,
            total_objetos INTEGER,
            confianca_media REAL)

            `);

            await pool.query(`
                CREATE TABLE IF NOT EXISTS caixa_delimitadora(
                id_caixa SERIAL PRIMARY KEY,
                id_sessao TEXT REFERENCES sessao_inferencia(id_sessao),
                rotulo_classe TEXT,
                confianca REAL,
                largura_px REAL,
                altura_px REAL,
                centroide_x REAL,
                centroide_y REAL,
                area_px2 REAL
                )

                `);

                console.log('Tabelas criadas');

    }

    catch(err){
        console.error('Erro ao criar tabelas', err.message);

    }
    
}

criarTabelas();

//recebe uma sessao de analise completa, desesturutra o json
app.post('/api/inferencia', async (req, res) =>{
    const {
        id_sessao,
        data_hora,
        nome_modelo,
        tempo_execucao_ms,
        total_objetos,
        confianca_media,
        caixas_delimitadoras,
    } = req.body;

    if(!id_sessao || !Array.isArray(caixas_delimitadoras)){
        return res.status(400).json({error: 'Campos ausentes ou invalidos'});
    }
   //evita sql injection
    try{
        await pool.query(
            `INSERT INTO sessao_inferencia(id_sessao, data_hora, nome_modelo, tempo_execucao_ms, total_objetos,confianca_media)
             VALUES ($1, $2, $3, $4, $5, $6)`,//vai ser substituido pelo valor real
              [id_sessao, data_hora, nome_modelo, tempo_execucao_ms, total_objetos, confianca_media]
        );

        for(const caixa of caixas_delimitadoras){
            await pool.query(
                `INSERT INTO caixa_delimitadora
                (id_sessao, rotulo_classe, confianca, largura_px, altura_px, centroide_x, centroide_y, area_px2)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,

                [
                    id_sessao,
                    caixa.rotulo_classe,
                    caixa.confianca,
                    caixa.largura_px,
                    caixa.altura_px,
                    caixa.centroide_x,
                    caixa.centroide_y,
                    caixa.area_px2

                ]
            );
        }

        res.status(201).json({
            message: 'Inferencia persistida',
            id_sessao,
            total_caixas_salvas: caixas_delimitadoras.length
        });

    }
    catch(err){
        console.error('Erro ao salvar inferencia', err.message);
        res.status(500).json({error: 'Erro ao salvar inferencia no banco'});
    }


});

//api get retorna a lista das spessoes salvas

app.get('/api/inferencia/historico', async (req,res) => {
    try{
        const resultado = await pool.query(
            `SELECT id_sessao, data_hora, nome_modelo, tempo_execucao_ms, total_objetos, confianca_media
            FROM sessao_inferencia
            ORDER BY data_hora DESC`
        );

        res.status(200).json(resultado.rows);
    }
    catch(err){
        console.error('Erro ao buscar historico', err.message);
        res.status(500).json({error:'Erro ao buscar historico'});
    }
});

// get q retorna os detakhes de uma sessao especifica e todas as caixas delimitadoras
app.get('/api/inferencia/:id_sessao/caixas', async (req, res) => {
    const{ id_sessao} = req.params;

    try{
        const sessaoResultado = await pool.query(
            `SELECT * FROM sessao_inferencia WHERE id_sessao = $1`,
      [id_sessao]
        );

        if(sessaoResultado.rows.length === 0){
            return res.status(404).json({error:'Sessao nao encontrada'});
        }

        const caixaResultado = await pool.query(
            `SELECT * FROM caixa_delimitadora WHERE id_sessao = $1`,
      [id_sessao]
        );

        res.status(200).json({
            sessao: sessaoResultado.rows[0],
            caixas_delimitadoras: caixaResultado.rows
        });
    }
    catch(err){
        console.error('Erro ao buscar caixas de sessao', err.message);
        res.status(500).json({error: 'Erro ao buscar os detalhes da sessao'});
    }
});

//inicia o servidor, uso 0000 pra celular poder acessar o servidor

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Servidor rodando em http://0.0.0.0:${PORT}`);
});