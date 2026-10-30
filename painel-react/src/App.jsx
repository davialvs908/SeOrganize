import { useMemo, useState } from 'react'
import { usePainel } from './usePainel.js'
import { filtrarSalas, resumir } from './painel.js'
import { Agenda, Cabecalho, Filtros, Resumo, SalaCard } from './components.jsx'
import { modoDemonstracao } from './api.js'

export default function App() {
  const { carregando, erro, dados, atualizadoEm, atualizar } = usePainel()
  const [filtro, setFiltro] = useState('todas')
  const [busca, setBusca] = useState('')

  const agora = atualizadoEm ?? new Date()
  const salasVisiveis = useMemo(
    () => (dados ? filtrarSalas(dados.salas, filtro, busca) : []),
    [dados, filtro, busca],
  )

  if (carregando) {
    return <main className="estado">Carregando painel…</main>
  }

  if (!dados) {
    return (
      <main className="estado">
        <p>Não foi possível carregar o painel: {erro}</p>
        <button type="button" className="botao" onClick={atualizar}>Tentar de novo</button>
      </main>
    )
  }

  return (
    <div className="pagina">
      <Cabecalho usuario={dados.usuario} atualizadoEm={atualizadoEm} onAtualizar={atualizar} demo={modoDemonstracao} />
      {erro && <p className="aviso" role="alert">Falha na última atualização: {erro}</p>}

      <Resumo resumo={resumir(dados.salas, dados.ocupacao, agora)} />

      <div className="conteudo">
        <section aria-label="Salas">
          <Filtros filtro={filtro} onFiltro={setFiltro} busca={busca} onBusca={setBusca} />
          {salasVisiveis.length === 0 ? (
            <p className="sub">Nenhuma sala encontrada com esse filtro.</p>
          ) : (
            <div className="grade">
              {salasVisiveis.map((sala) => (
                <SalaCard key={sala.id} sala={sala} agora={agora} />
              ))}
            </div>
          )}
        </section>
        <Agenda ocupacao={dados.ocupacao} agora={agora} />
      </div>

      <footer className="rodape">
        SeOrganize · Front-end em React consumindo a API REST do back-end Spring Boot
      </footer>
    </div>
  )
}
