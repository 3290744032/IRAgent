import request from './request'

interface LoginParams {
  account: string
  password: string
  verifiCode: string   // matches LoginDTO.verifiCode
  uuid: string          // from X-Verification-UUID header
}

interface LoginResult {
  token: string
  userId: string
  role: string // "admin" | "user"
}

export function login(params: LoginParams): Promise<LoginResult> {
  return request.post('/auth/login', params)
}
