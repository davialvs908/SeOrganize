// Dados simulados para a demonstração pública do painel (sem back-end).
// As reservas são geradas em relação ao horário atual, então a demo
// sempre mostra salas ocupadas, livres e próximas reuniões.

function iso(data) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${data.getFullYear()}-${pad(data.getMonth() + 1)}-${pad(data.getDate())}T${pad(data.getHours())}:${pad(data.getMinutes())}:00`
}

function emMinutos(base, minutos) {
  const d = new Date(base)
  d.setSeconds(0, 0)
  d.setMinutes(d.getMinutes() + minutos)
  return d
}

export function dadosDeExemplo(agora) {
  const salasBase = [
    { id: 1, nome: 'Sala Alpha', capacidade: 12, recursos: 'Projetor 4K, Videoconferência', localizacao: '10º andar' },
    { id: 2, nome: 'Sala Beta', capacidade: 6, recursos: 'TV 55", Quadro branco', localizacao: '10º andar' },
    { id: 3, nome: 'Sala Gama', capacidade: 4, recursos: 'Videoconferência', localizacao: '9º andar' },
    { id: 4, nome: 'Auditório', capacidade: 40, recursos: 'Som, Projetor, Microfones', localizacao: 'Térreo' },
  ]

  const reservas = [
    { id: 101, salaId: 1, solicitante: 'Ana Souza', inicio: emMinutos(agora, -25), fim: emMinutos(agora, 35) },
    { id: 102, salaId: 1, solicitante: 'Bruno Lima', inicio: emMinutos(agora, 90), fim: emMinutos(agora, 150) },
    { id: 103, salaId: 2, solicitante: 'Carla Mendes', inicio: emMinutos(agora, 20), fim: emMinutos(agora, 50) },
    { id: 104, salaId: 4, solicitante: 'Diretoria', inicio: emMinutos(agora, -10), fim: emMinutos(agora, 110) },
    { id: 105, salaId: 3, solicitante: 'Equipe de TI', inicio: emMinutos(agora, 180), fim: emMinutos(agora, 240) },
  ].map((r) => ({
    ...r,
    sala: salasBase.find((s) => s.id === r.salaId).nome,
    inicio: iso(r.inicio),
    fim: iso(r.fim),
  }))

  const agoraIso = iso(agora)
  const salas = salasBase.map((sala) => {
    const daSala = reservas.filter((r) => r.salaId === sala.id).sort((a, b) => a.inicio.localeCompare(b.inicio))
    const reservaAtual = daSala.find((r) => r.inicio <= agoraIso && r.fim > agoraIso) ?? null
    const proximaReserva = daSala.find((r) => r.inicio > agoraIso) ?? null
    return { ...sala, ocupadaAgora: Boolean(reservaAtual), reservaAtual, proximaReserva }
  })

  return {
    usuario: { nome: 'Visitante da Demo', perfil: 'COLABORADOR', iniciais: 'VD' },
    salas,
    ocupacao: reservas.sort((a, b) => a.inicio.localeCompare(b.inicio)),
  }
}
