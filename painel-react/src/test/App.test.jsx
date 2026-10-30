import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import App from '../App.jsx'

const dadosApi = {
  '/api/painel/me': { nome: 'Ana Souza', perfil: 'COLABORADOR', iniciais: 'AS' },
  '/api/painel/salas': [
    {
      id: 1, nome: 'Sala Alpha', capacidade: 12, recursos: 'Projetor', localizacao: '10º andar',
      ocupadaAgora: true,
      reservaAtual: { id: 7, salaId: 1, sala: 'Sala Alpha', solicitante: 'Bruno Lima', inicio: '2099-01-01T10:00:00', fim: '2099-01-01T11:00:00' },
      proximaReserva: null,
    },
    {
      id: 2, nome: 'Sala Beta', capacidade: 6, recursos: 'TV', localizacao: '9º andar',
      ocupadaAgora: false, reservaAtual: null, proximaReserva: null,
    },
  ],
  '/api/painel/ocupacao': [
    { id: 7, salaId: 1, sala: 'Sala Alpha', solicitante: 'Bruno Lima', inicio: '2099-01-01T10:00:00', fim: '2099-01-01T11:00:00' },
  ],
}

function mockarFetch() {
  vi.stubGlobal('fetch', vi.fn(async (url) => ({
    ok: true,
    status: 200,
    json: async () => dadosApi[url],
  })))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Painel de Salas', () => {
  it('consome a API e mostra o status de cada sala', async () => {
    mockarFetch()
    render(<App />)

    const alpha = await screen.findByRole('article', { name: 'Sala Alpha' })
    expect(within(alpha).getByText('Em uso')).toBeInTheDocument()
    expect(within(alpha).getByText('Bruno Lima')).toBeInTheDocument()

    const beta = screen.getByRole('article', { name: 'Sala Beta' })
    expect(within(beta).getByText('Livre')).toBeInTheDocument()

    expect(fetch).toHaveBeenCalledWith('/api/painel/salas', expect.objectContaining({ credentials: 'same-origin' }))
  })

  it('filtra para mostrar apenas as salas livres', async () => {
    mockarFetch()
    render(<App />)
    await screen.findByRole('article', { name: 'Sala Alpha' })

    fireEvent.click(screen.getByRole('button', { name: 'Livres' }))

    expect(screen.queryByRole('article', { name: 'Sala Alpha' })).not.toBeInTheDocument()
    expect(screen.getByRole('article', { name: 'Sala Beta' })).toBeInTheDocument()
  })

  it('mostra mensagem de erro quando a API falha', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 500, json: async () => ({}) })))
    render(<App />)

    expect(await screen.findByText(/Não foi possível carregar o painel/)).toBeInTheDocument()
  })
})
