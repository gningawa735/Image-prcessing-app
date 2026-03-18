<script setup lang="ts">
import { ref } from 'vue'
import { getImages, imageUrl } from '../http-api'

type ImageMeta = { id: number; name: string }

const images = ref<ImageMeta[]>([])

async function load() {
  images.value = await getImages()
}

await load()
</script>

<template>
  <div>
    <h2>Galerie d'images</h2>

    <p v-if="images.length === 0">Aucune image sur le serveur.</p>

    <div v-else class="gallery">
      <div v-for="image in images" :key="image.id" class="image-card">
        <p>{{ image.name }}</p>
        <img :src="imageUrl(image.id)" :alt="image.name" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.image-card {
  border: 1px solid #cccccc;
  padding: 8px;
  width: 220px;
}

.image-card img {
  max-width: 100%;
  height: auto;
  display: block;
}
</style>