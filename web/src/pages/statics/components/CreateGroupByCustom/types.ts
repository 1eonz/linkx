export interface Member {
  id: string
  name: string
  avatar?: string
}

export interface Department {
  id: string
  name: string
  children?: Department []
  members?: Member []
  _loaded?: boolean
}