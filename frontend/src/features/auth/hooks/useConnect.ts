import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useConnectionStore } from '@/lib/store/connections.store'
import { validateConnection } from '../api/auth.api'

export interface ConnectInput {
  label: string
  baseUrl: string
  apiKey: string
}

/**
 * Connecting is the app's login: validate the credentials against the backend,
 * and only store them once the backend has accepted them. The label falls back
 * to the product name the backend reports, so an unnamed connection still
 * reads sensibly in the switcher.
 */
export function useConnect() {
  const navigate = useNavigate()
  const addConnection = useConnectionStore((s) => s.addConnection)

  return useMutation({
    mutationFn: ({ baseUrl, apiKey }: ConnectInput) =>
      validateConnection(baseUrl.trim(), apiKey.trim()),
    onSuccess: (info, { label, baseUrl, apiKey }) => {
      addConnection({
        label: label.trim() || info.productName,
        baseUrl: baseUrl.trim(),
        apiKey: apiKey.trim(),
      })
      navigate('/')
    },
  })
}
