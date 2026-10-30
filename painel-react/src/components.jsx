import { hora, minutosAte, textoTempo } from './painel.js'

export function Cabecalho({ usuario, atualizadoEm, onAtualizar, demo }) {
  return (
    <header className="cabecalho">
      <div>
        <h1>Painel de Salas</h1>
        <p className="sub">
          Ocupação em tempo real{demo && <span className="selo">modo demonstração</span>}
        </p>
      </div>
      <div className="cabecalho-acoes">
        {atualizadoEm && (
          <span className="sub">Atualizado às {atualizadoEm.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</span>
        )}
        <button type="button" className="botao" onClick={onAtualizar}>Atualizar</button>
        {usuario && (
          <span className="avatar" title={usuario.nome} aria-label={`Usuário ${usuario.nome}`}>
            {usuario.iniciais}
          </span>
        )}
      </div>
    </header>
  )
}

export function Resumo({ resumo }) {
  const cards = [
    { rotulo: 'Salas livres agora', valor: resumo.livres, tom: 'verde' },
    { rotulo: 'Em uso agora', valor: resumo.ocupadas, tom: 'vermelho' },
    { rotulo: 'Reservas restantes', valor: resumo.restantesHoje, tom: 'azul' },
  ]
  return (
    <section className="resumo" aria-label="Resumo">
      {cards.map((c) => (
        <div key={c.rotulo} className={`kpi kpi-${c.tom}`}>
          <span className="kpi-valor">{c.valor}</span>
          <span className="kpi-rotulo">{c.rotulo}</span>
        </div>
      ))}
    </section>
  )
}

export function Filtros({ filtro, onFiltro, busca, onBusca }) {
  const opcoes = [
    ['todas', 'Todas'],
    ['livres', 'Livres'],
    ['ocupadas', 'Ocupadas'],
  ]
  return (
    <div className="filtros">
      <div className="segmentos" role="group" aria-label="Filtrar salas">
        {opcoes.map(([valor, rotulo]) => (
          <button
            key={valor}
            type="button"
            className={filtro === valor ? 'ativo' : ''}
            aria-pressed={filtro === valor}
            onClick={() => onFiltro(valor)}
          >
            {rotulo}
          </button>
        ))}
      </div>
      <input
        type="search"
        className="busca"
        placeholder="Buscar por sala, andar ou recurso"
        aria-label="Buscar salas"
        value={busca}
        onChange={(e) => onBusca(e.target.value)}
      />
    </div>
  )
}

export function SalaCard({ sala, agora }) {
  const { ocupadaAgora, reservaAtual, proximaReserva } = sala
  return (
    <article className={`sala ${ocupadaAgora ? 'ocupada' : 'livre'}`} aria-label={sala.nome}>
      <div className="sala-topo">
        <h3>{sala.nome}</h3>
        <span className={`status ${ocupadaAgora ? 'status-ocupada' : 'status-livre'}`}>
          {ocupadaAgora ? 'Em uso' : 'Livre'}
        </span>
      </div>
      <p className="sala-info">
        {sala.capacidade} pessoas{sala.localizacao && ` · ${sala.localizacao}`}
      </p>
      {sala.recursos && <p className="sala-recursos">{sala.recursos}</p>}

      <div className="sala-rodape">
        {ocupadaAgora && reservaAtual ? (
          <p>
            <strong>{reservaAtual.solicitante}</strong> até {hora(reservaAtual.fim)}
          </p>
        ) : proximaReserva ? (
          <p>
            Próxima: {hora(proximaReserva.inicio)} ({textoTempo(minutosAte(proximaReserva.inicio, agora))})
          </p>
        ) : (
          <p>Sem reservas no restante do dia</p>
        )}
      </div>
    </article>
  )
}

export function Agenda({ ocupacao, agora }) {
  const proximas = ocupacao.filter((r) => new Date(r.fim) > agora)
  return (
    <section className="agenda" aria-label="Agenda">
      <h2>Agenda</h2>
      {proximas.length === 0 ? (
        <p className="sub">Nenhuma reserva ativa.</p>
      ) : (
        <ol>
          {proximas.map((r) => {
            const emAndamento = new Date(r.inicio) <= agora
            return (
              <li key={r.id} className={emAndamento ? 'em-andamento' : ''}>
                <span className="agenda-hora">{hora(r.inicio)} – {hora(r.fim)}</span>
                <span className="agenda-sala">{r.sala}</span>
                <span className="agenda-quem">{r.solicitante}</span>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}
