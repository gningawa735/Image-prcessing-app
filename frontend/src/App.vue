<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
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
    <h1>M1b</h1>

    <nav>
      <RouterLink to="/">Accueil</RouterLink>
      |
      <RouterLink to="/upload">Upload</RouterLink>
      |
      <RouterLink to="/gallery">Galerie</RouterLink>
    </nav>

    <Suspense>
      <RouterView />
    </Suspense>

    <h1>Ma Galerie d'Images & Analyse</h1>

    <div class="main-layout">
      <div class="gallery-side">
        <ImageGallery
          ref="galleryRef"
          @select-image="(id) => (selectedId.value = id)"
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

<style scoped>
/* Remplacement frontend par TP Vue + config Vite et proxy backend */

#app {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  padding: 20px;
  color: #2c3e50;
}

.main-layout {
  display: flex;
  align-items: flex-start;
  gap: 40px;
}

.gallery-side {
  flex: 2;
  border-right: 1px solid #eee;
  padding-right: 20px;
}

.details-side {
  flex: 1;
  position: sticky;
  top: 20px;
}

nav {
  margin-bottom: 20px;
}
</style>
