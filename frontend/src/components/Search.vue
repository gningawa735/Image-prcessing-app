<script setup lang="ts">
import { ref } from 'vue';
import {
  getImages,
  imageUrl,
  searchImagesByAttributes,
  type ImageMeta,
  type ImageSearchAttributes,
} from '../http-api';

const allImages = ref<ImageMeta[]>([]);
const filteredImages = ref<ImageMeta[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const name = ref('');
const format = ref<'JPEG' | 'PNG' | ''>('');
const keyword = ref('');

async function loadAllImages() {
  try {
    allImages.value = await getImages();
    filteredImages.value = allImages.value;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Impossible de charger la liste des images.';
  }
}

async function onSearch() {
  loading.value = true;
  errorMessage.value = '';

  const attrs: ImageSearchAttributes = {};
  if (name.value.trim() !== '') attrs.name = name.value.trim();
  if (format.value !== '') attrs.format = format.value as 'JPEG' | 'PNG';
  if (keyword.value.trim() !== '') attrs.keywords = [keyword.value.trim()];

  try {
    const ids = await searchImagesByAttributes(attrs);
    filteredImages.value = allImages.value.filter((img) =>
      ids.indexOf(img.id) !== -1
    );
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Erreur lors de la recherche (voir console/Network).';
  } finally {
    loading.value = false;
  }
}

await loadAllImages();
</script>

<template>
  <div>
    <h2>Recherche par attributs</h2>

    <form @submit.prevent="onSearch">
      <div>
        <label>Nom du fichier :</label>
        <input v-model="name" type="text" />
      </div>

      <div>
        <label>Format :</label>
        <select v-model="format">
          <option value="">(indifférent)</option>
          <option value="JPEG">JPEG</option>
          <option value="PNG">PNG</option>
        </select>
      </div>

      <div>
        <label>Mot-clé :</label>
        <input v-model="keyword" type="text" />
      </div>

      <button type="submit" :disabled="loading">
        {{ loading ? 'Recherche…' : 'Rechercher' }}
      </button>
    </form>

    <p v-if="errorMessage">{{ errorMessage }}</p>

    <h3>Résultats</h3>
    <p v-if="!filteredImages.length">Aucune image trouvée.</p>

    <div
      v-else
      style="display: flex; flex-wrap: wrap; gap: 12px; margin-top: 12px;"
    >
      <div
        v-for="img in filteredImages"
        :key="img.id"
        style="border: 1px solid #ccc; padding: 8px;"
      >
        <p>{{ img.name }} (id={{ img.id }})</p>
        <img :src="imageUrl(img.id)" alt="" style="max-width: 200px;" />
      </div>
    </div>
  </div>
</template>
