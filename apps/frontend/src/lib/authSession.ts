type SessionExpiredHandler = () => void | Promise<void>
type TokensRefreshedHandler = () => void | Promise<void>

const sessionExpiredHandlers = new Set<SessionExpiredHandler>()
const tokensRefreshedHandlers = new Set<TokensRefreshedHandler>()

export function onSessionExpired(handler: SessionExpiredHandler) {
  sessionExpiredHandlers.add(handler)

  return () => {
    sessionExpiredHandlers.delete(handler)
  }
}

export function notifySessionExpired() {
  for (const handler of sessionExpiredHandlers) {
    void handler()
  }
}

export function onTokensRefreshed(handler: TokensRefreshedHandler) {
  tokensRefreshedHandlers.add(handler)

  return () => {
    tokensRefreshedHandlers.delete(handler)
  }
}

export function notifyTokensRefreshed() {
  for (const handler of tokensRefreshedHandlers) {
    void handler()
  }
}
