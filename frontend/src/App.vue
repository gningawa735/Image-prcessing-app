<script setup lang="ts">
import { ref } from 'vue'
import ImageGallery from './components/ImageGallery.vue'
import ImageDetails from './components/ImageDetails.vue'

// L'ID qui fait le pont entre les deux composants
const selectedId = ref<number | null>(null)
</script>

<template>
  <div id="app">
    <h1>Notre Galerie & Analyse</h1>
    
    <div class="main-layout">
      <div class="gallery-side">
        <ImageGallery @select-image="(id) => selectedId = id" />
      </div>
      
      <div class="details-side">
        <ImageDetails :image-id="selectedId" />
      </div>
    </div>
  </div>
</template>

<style>
#app {
  font-family: sans-serif;
  padding: 20px;
}

.main-layout {
  display: flex;
  align-items: flex-start; /* Aligne le haut des deux colonnes */
  gap: 40px;
}

.gallery-side {
  flex: 2; /* Donne plus de place à la galerie */
  border-right: 1px solid #eee;
  padding-right: 20px;
}

.details-side {
  flex: 1; /* Le panneau de détails est plus étroit */
  position: sticky; /* Garde les détails visibles même si on scrolle la galerie */
  top: 20px;
}
</style>