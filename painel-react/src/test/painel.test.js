import { describe, expect, it } from 'vitest'
import { filtrarSalas, hora, resumir, textoTempo } from '../painel.js'

const salas = [
  { id: 1, nome: 'Sala Alpha', localizacao: '10º andar', recursos: 'Projetor', ocupadaAgora: true },
  { id: 2, nome: 'Sala Beta', localizacao: '10º andar', recursos: 'TV', ocupadaAgora: false },
  { id: 3, nome: 'Auditório', localizacao: 'Térreo', recursos: 'Som, Projetor', ocupadaAgora: false },
]

describe('filtrarSalas', () => {
  it('mostra só as livres', () => {
    expect(filtrarSalas(salas, 'livres', '').map((s) => s.id)).toEqual([2, 3])
  })

  it('mostra só as ocupadas', () => {
    expect(filtrarSalas(salas, 'ocupadas', '').map((s) => s.id)).toEqual([1])
  })

  it('busca por recurso sem diferenciar maiúsculas', () => {
    expect(filtrarSalas(salas, 'todas', 'PROJETOR').map((s) => s.id)).toEqual([1, 3])
  })

  it('combina filtro e busca', () => {
    expect(filtrarSalas(salas, 'livres', 'projetor').map((s) => s.id)).toEqual([3])
  })
})

describe('resumir', () => {
  it('conta salas livres, ocupadas e reservas que ainda não terminaram', () => {
    const agora = new Date('2026-11-03T10:30:00')
    const ocupacao = [
      { id: 1, inicio: '2026-11-03T10:00:00', fim: '2026-11-03T11:00:00' },
      { id: 2, inicio: '2026-11-03T14:00:00', fim: '2026-11-03T15:00:00' },
      { id: 3, inicio: '2026-11-03T08:00:00', fim: '2026-11-03T09:00:00' },
    ]
    expect(resumir(salas, ocupacao, agora)).toEqual({ total: 3, ocupadas: 1, livres: 2, restantesHoje: 2 })
  })
})

describe('formatação', () => {
  it('extrai a hora de uma data local', () => {
    expect(hora('2026-11-03T14:05:00')).toBe('14:05')
  })

  it('descreve quanto falta para a próxima reunião', () => {
    expect(textoTempo(0)).toBe('agora')
    expect(textoTempo(25)).toBe('em 25 min')
    expect(textoTempo(60)).toBe('em 1h')
    expect(textoTempo(95)).toBe('em 1h35')
  })
})
