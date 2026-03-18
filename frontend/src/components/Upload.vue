<script setup lang="ts">
import { ref } from 'vue'
import { uploadImage } from '../http-api'

const file = ref<File | null>(null)
const message = ref('')

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const selectedFile = target.files?.[0]

  if (selectedFile !== undefined) {
    file.value = selectedFile
  }
}

async function sendImage() {
  if (file.value === null) {
    message.value = 'Veuillez choisir une image.'
    return
  }

  try {
    await uploadImage(file.value)
    message.value = 'Image envoyee avec succes.'
  } catch (error) {
    message.value = "Erreur lors de l'envoi de l'image."
    console.error(error)
  }
}
</script>

<template>
  <div>
    <h2>Deposer une image</h2>

    <input type="file" accept="image/*" @change="onFileChange" />
    <button @click="sendImage">Envoyer</button>

    <p>{{ message }}</p>
  </div>
</template>