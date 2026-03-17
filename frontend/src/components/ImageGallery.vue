<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
defineEmits(['select-image'])

interface ImageItem {
  id: number
  name: string
}

const images = ref<ImageItem[]>([])
const error = ref<string | null>(null)

const refreshGallery = async () => {
  try {
    // Appelle l'endpoint GET /images défini dans ImageController.java
    const response = await axios.get('http://localhost:8080/images')
    images.value = response.data
  } catch (err) {
    error.value = "Impossible de charger la galerie."
  }
}

onMounted(() => {
  refreshGallery()
})
</script>

<template>
  <div class="gallery-container">
    <p v-if="error" class="error">{{ error }}</p>
    
    <div class="gallery-grid">
      <div v-for="img in images" :key="img.id" class="vignette" @click="$emit('select-image', img.id)">
        <img :src="'http://localhost:8080/images/' + img.id" :alt="img.name" />
        <span class="image-name">{{ img.name }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gallery-grid {
  display: grid;
  /* repeat(auto-fill, ...) permet d'aligner autant de vignettes que possible par ligne */
  grid-template-columns: repeat(auto-fill, 150px);
  gap: 20px;
  justify-content: center;
}

/* CONTRAINTE : Taille fixe de la vignette */
.vignette {
  width: 150px;
  height: 150px;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #fcfcfc;
  overflow: hidden;
}

/* Remplissage en hauteur ou largeur */
img {
  width: 100%;
  height: 80%; /* Laisse de la place pour le nom en bas */
  
  /* L'image garde ses proportions sans être écrasée */
  object-fit: contain; 
  background-color: #eee;
}

.image-name {
  font-size: 0.75rem;
  padding: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis; /* Coupe le texte proprement s'il est trop long */
  width: 90%;
  text-align: center;
}

.error { color: red; text-align: center; }
</style>