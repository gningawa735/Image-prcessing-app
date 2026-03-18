<script setup lang="ts">
import { ref } from 'vue'
import ImageGallery from './components/ImageGallery.vue'
import ImageDetails from './components/ImageDetails.vue'

const selectedId = ref<number | null>(null)

// On crée une référence pour "viser" le composant ImageGallery
const galleryRef = ref<any>(null)

/**
 * Fonction de rafraîchissement global (Besoin 20)
 * Appellée quand ImageDetails émet 'image-deleted'
 */
const refreshAll = () => {
  selectedId.value = null 
  
  // On demande à la galerie de recharger sa liste depuis le serveur
  if (galleryRef.value && galleryRef.value.fetchImages) {
    galleryRef.value.fetchImages()
  }
}
</script>

<template>
  <div id="app">
    <h1>Ma Galerie d'Images & Analyse</h1>
    
    <div class="main-layout">
      <div class="gallery-side">
        <ImageGallery 
          ref="galleryRef" 
          @select-image="(id) => selectedId = id" 
        />
      </div>
      
      <div class="details-side">
        <ImageDetails 
          :image-id="selectedId"
          @image-deleted="refreshAll" 
        />
      </div>
    </div>
  </div>
</template>

<style>
#app { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #2c3e50; }
.main-layout { display: flex; align-items: flex-start; gap: 40px; }
.gallery-side { flex: 2; border-right: 1px solid #eee; padding-right: 20px; }
.details-side { flex: 1; position: sticky; top: 20px; }
</style>