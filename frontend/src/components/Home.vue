<script setup lang="ts">
import { ref } from 'vue'
import { getImages, imageUrl } from '../http-api'

type ImageMeta = { id: number; name: string }

const images = ref<ImageMeta[]>([])
const selectedId = ref<number | null>(null)

async function load() {
  images.value = await getImages()
  if (images.value.length > 0) {
    const firstImage = images.value[0]
    if (firstImage !== undefined) {
      selectedId.value = firstImage.id
    }
  }
}

function selectedSrc() {
  return selectedId.value === null ? '' : imageUrl(selectedId.value)
}

await load()
</script>

<template>
  <div>
    <h2>Choisir une image</h2>

    <select v-if="images.length" v-model="selectedId">
      <option v-for="img in images" :key="img.id" :value="img.id">
        {{ img.name }} (id={{ img.id }})
      </option>
    </select>

    <p v-else>Aucune image sur le serveur.</p>

    <div v-if="selectedId !== null" style="margin-top: 16px;">
      <img :src="selectedSrc()" alt="Selected" style="max-width: 600px;" />
    </div>
  </div>
</template>