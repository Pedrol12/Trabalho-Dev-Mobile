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