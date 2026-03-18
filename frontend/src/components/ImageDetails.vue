<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'

const props = defineProps<{ imageId: number | null }>()
const similarImages = ref<any[]>([])
const imageDetails = ref<any>(null)
const numSimilar = ref(4)
const descriptor = ref('H1D')
const emit = defineEmits(['image-deleted', 'select-image']);

// --- LOGIQUE DE CALCUL ---

// Fonction de normalisation pour avoir un score entre 0 et 1
const normalizeScore = (rawScore: number) => {
  if (rawScore <= 0) return "1.0000";
  // Avec tes descripteurs désormais normalisés sur le Backend, 
  // un sigma de 0.5 est une bonne base de départ.
  const sigma = 0.5; 
  const similarity = Math.exp(-rawScore / sigma);
  return similarity.toFixed(4);
}

// --- APPELS API ---

const fetchDetails = async () => {
  if (!props.imageId) return
  try {
    const res = await axios.get(`http://localhost:8080/images/${props.imageId}/info`)
    imageDetails.value = res.data
  } catch (err) {
    console.error("Erreur détails", err)
    imageDetails.value = null
  }
}

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

// --- ACTIONS ---

const downloadImage = async () => {
  if (!props.imageId || !imageDetails.value) return
  try {
    const response = await axios.get(`http://localhost:8080/images/${props.imageId}`, {
      responseType: 'blob' 
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', imageDetails.value.name)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    alert("Erreur lors du téléchargement")
  }
}

const deleteImage = async () => {
  if (!props.imageId) return;

  try {
    // Appel de l'API de suppression sur le backend
    await axios.delete(`http://localhost:8080/images/${props.imageId}`);
    
    // Message de succès 
    alert("Image supprimée avec succès !");
    
    emit('image-deleted'); 
    
  } catch (err) {
    // 5. Gestion d'erreur si le serveur ne répond pas ou si le fichier est protégé
    console.error("Erreur lors de la suppression :", err);
    alert("Impossible de supprimer l'image.");
  }
};


watch(() => props.imageId, () => {
  fetchDetails()
  fetchSimilar()
})

watch(() => [descriptor.value, numSimilar.value], fetchSimilar)

</script>

<template>
  <div v-if="imageId" class="details-panel">
    <h3>Image sélectionnée : #{{ imageId }}</h3>
    <img :src="'http://localhost:8080/images/' + imageId" class="preview-big" />

    <div class="controls"> 
      <label>Descripteur :</label>
      <select v-model="descriptor">
        <option value="H1D">Texture (H1D)</option>
        <option value="H2D">Couleur HSV (H2D)</option>
        <option value="H3D">Couleur RGB (H3D)</option>
      </select>
      
      <label>Nombre :</label>
      <input type="number" v-model="numSimilar" min="1" max="10" />
    </div>

    <div v-if="imageDetails" class="metadata-section">
      <h4>Informations</h4>
      <ul>
        <li><strong>Nom :</strong> {{ imageDetails.name }}</li>
        <li><strong>Taille :</strong> {{ (imageDetails.size / 1024).toFixed(2) }} Ko</li>
      </ul>
    </div>

    <h4>Images similaires :</h4>
    <div class="similar-grid">
      <div v-for="sim in similarImages" :key="sim.id" class="sim-card">
        <img :src="'http://localhost:8080/images/' + sim.id" />
        <span class="score">
           Similarité : {{ normalizeScore(sim.score) }}
        </span>     
      </div>
    </div>

    <div class="actions-bar">
        <button @click="downloadImage" class="btn-save">Sauvegarder</button>
        <button @click="deleteImage" class="btn-delete">Supprimer</button>
    </div>
  </div>
</template>

<style scoped>
    .details-panel { padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
    .preview-big { max-width: 100%; height: auto; border: 3px solid #42b983; margin-bottom: 20px; }
    .similar-grid { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
    .sim-card { display: flex; flex-direction: column; width: 100px; text-align: center; }
    .sim-card img { width: 100px; height: 100px; object-fit: cover; border-radius: 4px; }
    .score { font-size: 0.75rem; color: #666; margin-top: 5px; }
    .actions-bar { display: flex; gap: 10px; border-top: 1px solid #eee; padding-top: 20px; }
    .btn-save { background-color: #2c3e50; color: white; border: none; padding: 10px; cursor: pointer; border-radius: 4px; flex: 1; }
    .btn-delete { background-color: #e74c3c; color: white; border: none; padding: 10px; cursor: pointer; border-radius: 4px; flex: 1; }
    .btn-delete:hover { background-color: #c0392b; }
</style>