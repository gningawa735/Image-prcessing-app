<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

// On déclare l'événement pour que le parent sache quand une image est cliquée
const emit = defineEmits(['select-image'])

interface ImageItem {
  id: number
  name: string
}

const images = ref<ImageItem[]>([])
const error = ref<string | null>(null)

/**
 * CHARGEMENT DE LA GALERIE (Besoin 1)
 * Récupère la liste de toutes les images stockées sur le serveur
 */
const fetchImages = async () => {
  try {
    // Appel à l'API Backend (ImageController.java)
    const response = await axios.get('http://localhost:8080/images')
    images.value = response.data
    error.value = null // On réinitialise l'erreur en cas de succès
  } catch (err) {
    error.value = "Impossible de charger la galerie."
    console.error(err)
  }
}

// Au chargement initial du composant, on récupère les images
onMounted(() => {
  fetchImages()
})

/**
 * EXPOSITION (Crucial pour le Besoin 20)
 * On rend la fonction fetchImages accessible depuis App.vue via une "ref"
 */
defineExpose({ fetchImages });
</script>

<template>
  <div class="gallery-container">
    <p v-if="error" class="error">{{ error }}</p>
    
    <div class="gallery-grid">
      <div 
        v-for="img in images" 
        :key="img.id" 
        class="vignette" 
        @click="emit('select-image', img.id)"
      >
        <img :src="'http://localhost:8080/images/' + img.id" :alt="img.name" />
        <span class="image-name">{{ img.name }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Tes styles sont parfaits : le grid auto-fill est la meilleure pratique pour une galerie */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 150px);
  gap: 20px;
  justify-content: center;
}

.vignette {
  width: 150px;
  height: 150px;
  border: 1px solid #ddd;
  border-radius: 8px; /* Un peu plus arrondi c'est plus moderne */
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #fff;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

/* Petit effet de survol pour l'expérience utilisateur */
.vignette:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

img {
  width: 100%;
  height: 80%; 
  object-fit: cover; /* "cover" remplit mieux la vignette que "contain" pour une galerie */
  background-color: #eee;
}

.image-name {
  font-size: 0.75rem;
  padding: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 90%;
  text-align: center;
  color: #333;
}

.error { color: #e74c3c; text-align: center; font-weight: bold; }
</style>