<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'

const props = defineProps<{ imageId: number | null }>()
const similarImages = ref<any[]>([])
const imageDetails = ref<any>(null) // Ajoute ceci pour stocker les méta-données
const numSimilar = ref(4)
const descriptor = ref('H1D')

// Fonction 1 : Récupérer les méta-données (Besoin 17)
const fetchDetails = async () => {
  if (!props.imageId) return
  try {
    // Appelle la route info du backend
    const res = await axios.get(`http://localhost:8080/images/${props.imageId}/info`)
    imageDetails.value = res.data
  } catch (err) {
    console.error("Erreur détails", err)
    imageDetails.value = null
  }
}

// Fonction 2 : Récupérer les images similaires (Besoin 17)
const fetchSimilar = async () => {
  if (!props.imageId) return
  try {
    const res = await axios.get(`http://localhost:8080/images/${props.imageId}/similar`, {
      params: { number: numSimilar.value, descriptor: descriptor.value }
    })
    similarImages.value = res.data
  } catch (err) {
    console.error("Erreur similarité", err)
  }
}

const normalizeScore = (rawScore: number) => {
  // Plus le diviseur est grand, plus le score restera haut. 
  // On ajuste selon le descripteur pour avoir des scores entre 0 et 1
  const sigma = 1000000; 
  const similarity = Math.exp(-rawScore / sigma);
  return similarity.toFixed(3); // Retourne un chiffre comme 0.854
}

// On surveille les changements pour tout mettre à jour
watch(() => props.imageId, () => {
  fetchDetails() // On rafraîchit les infos
  fetchSimilar() // On rafraîchit les similarités
})

// On surveille aussi les réglages (descripteur/nombre)
watch(() => [descriptor.value, numSimilar.value], fetchSimilar)
</script>

<template>
  <div v-if="imageId" class="details-panel">
    <h3>Image sélectionnée : {{ imageId }}</h3>
    <img :src="'http://localhost:8080/images/' + imageId" class="preview-big" />

    <div class="controls"> <label>Descripteur :</label>
      <select v-model="descriptor">
        <option value="H1D">Texture (H1D)</option>
        <option value="H2D">Couleur HSV (H2D)</option>
        <option value="H3D">Couleur RGB (H3D)</option>
      </select>
      
      <label>Nombre :</label>
      <input type="number" v-model="numSimilar" min="1" max="10" />
    </div>

    <div v-if="imageDetails" class="metadata-section">
    <h3>Informations sur l'image</h3>
    <ul>
      <li><strong>Nom :</strong> {{ imageDetails.name }}</li>
      <li><strong>Identifiant :</strong> #{{ imageDetails.id }}</li>
      <li><strong>Taille :</strong> {{ (imageDetails.size / 1024).toFixed(2) }} Ko</li>
    </ul>
  </div>

    <h4>Images similaires :</h4>
    <div class="similar-grid">
      <div v-for="sim in similarImages" :key="sim.id" class="sim-card">
        <img :src="'http://localhost:8080/images/' + sim.id" />
        <span class="score">Similarité : {{ normalizeScore(sim.score) }}</span>      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-big { max-width: 300px; border: 3px solid #42b983; }
.similar-grid { display: flex; gap: 10px; flex-wrap: wrap; }
.sim-card { display: flex; flex-direction: column; font-size: 0.7rem; }
.sim-card img { width: 80px; height: 80px; object-fit: cover; }
.score { color: #666; font-weight: bold; }
</style>