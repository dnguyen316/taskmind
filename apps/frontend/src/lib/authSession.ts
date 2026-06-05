type SessionExpiredHandler = () => void | Promise<void>

const sessionExpiredHandlers = new Set<SessionExpiredHandler>()

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
