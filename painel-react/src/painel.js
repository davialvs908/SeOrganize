// Regras de apresentação do painel, separadas dos componentes para serem testadas.

export function dataLocal(data) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${data.getFullYear()}-${pad(data.getMonth() + 1)}-${pad(data.getDate())}`
}

export function hora(isoLocal) {
  if (!isoLocal) return ''
  return isoLocal.slice(11, 16)
}

export function minutosAte(isoLocal, agora) {
  const alvo = new Date(isoLocal)
  return Math.round((alvo.getTime() - agora.getTime()) / 60000)
}

export function textoTempo(minutos) {
  if (minutos <= 0) return 'agora'
  if (minutos < 60) return `em ${minutos} min`
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  return m === 0 ? `em ${h}h` : `em ${h}h${String(m).padStart(2, '0')}`
}

export function resumir(salas, ocupacao, agora) {
  const ocupadas = salas.filter((s) => s.ocupadaAgora).length
  const hoje = dataLocal(agora)
  const restantesHoje = ocupacao.filter((r) => new Date(r.fim) > agora && r.inicio.slice(0, 10) <= hoje).length
  return {
    total: salas.length,
    ocupadas,
    livres: salas.length - ocupadas,
    restantesHoje,
  }
}

export const FILTROS = {
  todas: () => true,
  livres: (sala) => !sala.ocupadaAgora,
  ocupadas: (sala) => sala.ocupadaAgora,
}

export function filtrarSalas(salas, filtro, busca) {
  const termo = (busca ?? '').trim().toLowerCase()
  return salas
    .filter(FILTROS[filtro] ?? FILTROS.todas)
    .filter((sala) => {
      if (!termo) return true
      return [sala.nome, sala.localizacao, sala.recursos]
        .filter(Boolean)
        .some((campo) => campo.toLowerCase().includes(termo))
    })
}
