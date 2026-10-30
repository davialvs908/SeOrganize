import { useCallback, useEffect, useState } from 'react'
import { carregarPainel, NaoAutenticadoError } from './api.js'

const INTERVALO_MS = 30_000

/** Carrega os dados do painel e atualiza sozinho a cada 30 segundos. */
export function usePainel() {
  const [estado, setEstado] = useState({ carregando: true, erro: null, dados: null, atualizadoEm: null })

  const atualizar = useCallback(async () => {
    try {
      const dados = await carregarPainel()
      setEstado({ carregando: false, erro: null, dados, atualizadoEm: new Date() })
    } catch (erro) {
      if (erro instanceof NaoAutenticadoError) {
        window.location.assign('/')
        return
      }
      setEstado((anterior) => ({ ...anterior, carregando: false, erro: erro.message }))
    }
  }, [])

  useEffect(() => {
    atualizar()
    const id = setInterval(atualizar, INTERVALO_MS)
    return () => clearInterval(id)
  }, [atualizar])

  return { ...estado, atualizar }
}
