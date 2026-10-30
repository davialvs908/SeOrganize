import { dadosDeExemplo } from './mock.js'

const USAR_MOCK = import.meta.env.VITE_USAR_MOCK === 'true'

export class NaoAutenticadoError extends Error {}

async function getJson(caminho) {
  const resposta = await fetch(caminho, {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })
  if (resposta.status === 401) {
    throw new NaoAutenticadoError('Sessão expirada')
  }
  if (!resposta.ok) {
    throw new Error(`Erro ${resposta.status} ao carregar ${caminho}`)
  }
  return resposta.json()
}

/** Busca tudo que o painel precisa em paralelo. */
export async function carregarPainel() {
  if (USAR_MOCK) {
    return dadosDeExemplo(new Date())
  }
  const [usuario, salas, ocupacao] = await Promise.all([
    getJson('/api/painel/me'),
    getJson('/api/painel/salas'),
    getJson('/api/painel/ocupacao'),
  ])
  return { usuario, salas, ocupacao }
}

export const modoDemonstracao = USAR_MOCK
