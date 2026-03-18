import axios from 'axios'

export type ImageMeta = { id: number; name: string }

export async function getImages(): Promise<ImageMeta[]> {
  const res = await axios.get<ImageMeta[]>('/images')
  return res.data
}

export function imageUrl(id: number): string {
  return `/images/${id}`
}

export async function uploadImage(file: File): Promise<void> {
  const formData = new FormData()
  //le backend attend un fichier dans le champ "file"
  formData.append('file', file)
  await axios.post('/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function deleteImage(id: number): Promise<void> {
  await axios.delete(`/images/${id}`)
}
