import request from './request'

export interface FlaggedQuestion {
  id: string
  questionText: string
  correctAnswer: string
  topic: string // knowledge_point → topic
  source: string
  flaggedAt: string
}

export interface FlaggedListResult {
  data: FlaggedQuestion[]
  total: number
}

export function getFlaggedQuestions(page: number, size: number): Promise<FlaggedListResult> {
  return request.get('/admin/questions/flagged', { params: { page, size } })
}

export function reviewQuestion(id: string, action: 'approve' | 'reject'): Promise<{ success: boolean; status: string }> {
  return request.put(`/admin/questions/${id}/review`, { action })
}
