type E2eAuthCredentials = {
  email: string
  password: string
  otp: string
}

export function e2eAuthCredentials(): E2eAuthCredentials {
  const credentials = {
    email: process.env.E2E_AUTH_EMAIL,
    password: process.env.E2E_AUTH_PASSWORD,
    otp: process.env.E2E_AUTH_OTP,
  }

  for (const [name, value] of Object.entries(credentials)) {
    if (!value) {
      throw new Error(
        `Missing E2E_AUTH_${name.toUpperCase()} in the browser test runner environment`,
      )
    }
  }

  return credentials as E2eAuthCredentials
}
