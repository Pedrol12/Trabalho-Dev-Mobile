
require('dotenv').config();

const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Conexão com o PostgreSQL.

const pool = new Pool(
    process.env.DATABASE_URL
        ? {
            connectionString: process.env.DATABASE_URL
        }
        : {
            host: process.env.DB_HOST,
            port: process.env.DB_PORT,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        }
);

// Cria as tabelas e atualiza a estrutura,
// sem apagar os dados já existentes.
async function criarTabelas() {

    await pool.query(`
        CREATE TABLE IF NOT EXISTS sessao_inferencia (
            id_sessao TEXT PRIMARY KEY,
            data_hora TEXT,
            nome_modelo TEXT,
            tempo_execucao_ms INTEGER,
            total_objetos INTEGER,
            confianca_media REAL,
            largura_imagem_px INTEGER,
            altura_imagem_px INTEGER
        )
    `);

    // Atualiza bancos criados pela versão antiga.
    await pool.query(`
        ALTER TABLE sessao_inferencia
        ADD COLUMN IF NOT EXISTS
        largura_imagem_px INTEGER
    `);

    await pool.query(`
        ALTER TABLE sessao_inferencia
        ADD COLUMN IF NOT EXISTS
        altura_imagem_px INTEGER
    `);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS caixa_delimitadora (
            id_caixa SERIAL PRIMARY KEY,
            id_sessao TEXT REFERENCES
                sessao_inferencia(id_sessao),
            rotulo_classe TEXT,
            confianca REAL,
            largura_px REAL,
            altura_px REAL,
            centroide_x REAL,
            centroide_y REAL,
            area_px2 REAL
        )
    `);

    console.log('Tabelas verificadas e atualizadas.');
}

// Recebe uma sessão de inferência do aplicativo.
app.post('/api/inferencia', async (req, res) => {

    const {
        id_sessao,
        data_hora,
        nome_modelo,
        tempo_execucao_ms,
        total_objetos,
        confianca_media,
        dimensao_imagem,
        caixas_delimitadoras
    } = req.body || {};

    if (
        !id_sessao ||
        !Array.isArray(caixas_delimitadoras)
    ) {
        return res.status(400).json({
            error: 'Campos ausentes ou inválidos.'
        });
    }

    // Mantém compatibilidade com versões anteriores
    // do aplicativo, que ainda não enviam dimensões.
    let larguraImagem = null;
    let alturaImagem = null;

    if (dimensao_imagem != null) {

        const largura = dimensao_imagem.largura_px;
        const altura = dimensao_imagem.altura_px;

        if (
            !Number.isInteger(largura) ||
            !Number.isInteger(altura) ||
            largura <= 0 ||
            altura <= 0
        ) {
            return res.status(400).json({
                error: 'Dimensões da imagem inválidas.'
            });
        }

        larguraImagem = largura;
        alturaImagem = altura;
    }

    let client;

    try {

        client = await pool.connect();

        // Inicia uma transação.
        await client.query('BEGIN');

        // Salva os dados gerais da sessão.
        await client.query(
            `
            INSERT INTO sessao_inferencia (
                id_sessao,
                data_hora,
                nome_modelo,
                tempo_execucao_ms,
                total_objetos,
                confianca_media,
                largura_imagem_px,
                altura_imagem_px
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            `,
            [
                id_sessao,
                data_hora,
                nome_modelo,
                tempo_execucao_ms,
                total_objetos,
                confianca_media,
                larguraImagem,
                alturaImagem
            ]
        );

        // Salva todas as caixas delimitadoras.
        for (const caixa of caixas_delimitadoras) {

            await client.query(
                `
                INSERT INTO caixa_delimitadora (
                    id_sessao,
                    rotulo_classe,
                    confianca,
                    largura_px,
                    altura_px,
                    centroide_x,
                    centroide_y,
                    area_px2
                )
                VALUES (
                    $1, $2, $3, $4,
                    $5, $6, $7, $8
                )
                `,
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

        // Confirma o salvamento de todos os dados.
        await client.query('COMMIT');

        res.status(201).json({
            message: 'Inferencia persistida',
            id_sessao,
            total_caixas_salvas:
                caixas_delimitadoras.length
        });

    } catch (err) {

        if (client) {
            try {
                await client.query('ROLLBACK');
            } catch (rollbackError) {
                console.error(
                    'Erro ao desfazer transação:',
                    rollbackError.message
                );
            }
        }

        console.error(
            'Erro ao salvar inferência:',
            err.message
        );

        res.status(500).json({
            error: 'Erro ao salvar inferência no banco.'
        });

    } finally {

        if (client) {
            client.release();
        }
    }
});

// Retorna o histórico de sessões.
app.get('/api/inferencia/historico', async (req, res) => {

    try {

        const resultado = await pool.query(`
            SELECT
                id_sessao,
                data_hora,
                nome_modelo,
                tempo_execucao_ms,
                total_objetos,
                confianca_media
            FROM sessao_inferencia
            ORDER BY data_hora DESC
        `);

        res.status(200).json(resultado.rows);

    } catch (err) {

        console.error(
            'Erro ao buscar histórico:',
            err.message
        );

        res.status(500).json({
            error: 'Erro ao buscar histórico.'
        });
    }
});

// Retorna os detalhes de uma sessão.
app.get(
    '/api/inferencia/:id_sessao/caixas',
    async (req, res) => {

        const { id_sessao } = req.params;

        try {

            const sessaoResultado = await pool.query(
                `
                SELECT *
                FROM sessao_inferencia
                WHERE id_sessao = $1
                `,
                [id_sessao]
            );

            if (sessaoResultado.rows.length === 0) {
                return res.status(404).json({
                    error: 'Sessão não encontrada.'
                });
            }

            const caixasResultado = await pool.query(
                `
                SELECT *
                FROM caixa_delimitadora
                WHERE id_sessao = $1
                ORDER BY id_caixa ASC
                `,
                [id_sessao]
            );

            res.status(200).json({
                sessao: sessaoResultado.rows[0],
                caixas_delimitadoras: caixasResultado.rows
            });

        } catch (err) {

            console.error(
                'Erro ao buscar detalhes:',
                err.message
            );

            res.status(500).json({
                error: 'Erro ao buscar os detalhes da sessão.'
            });
        }
    }
);

// Inicializa o servidor somente após
// verificar a estrutura do banco de dados.
async function iniciarServidor() {

    try {

        await criarTabelas();

        app.listen(PORT, '0.0.0.0', () => {
            console.log(
                `Servidor rodando em http://0.0.0.0:${PORT}`
            );
        });

    } catch (err) {

        console.error(
            'Erro ao inicializar servidor:',
            err.message
        );

        process.exit(1);
    }
}

iniciarServidor();
