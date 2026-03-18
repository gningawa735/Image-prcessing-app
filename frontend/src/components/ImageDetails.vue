<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import axios from 'axios'

const props = defineProps<{ imageId: number | null }>()
const similarImages = ref<any[]>([])
const imageDetails = ref<any>(null)
const numSimilar = ref(4)
const descriptor = ref('H1D')
const emit = defineEmits(['image-deleted', 'select-image']);

const newTag = ref("")
const existingTags = ref<string[]>([]) // Liste globale (Besoin 14)


const normalizeScore = (rawScore: number) => {
  if (rawScore <= 0) return "1.0000";
  const sigma = 0.5; 
  const similarity = Math.exp(-rawScore / sigma);
  return similarity.toFixed(4);
}

const fetchDetails = async () => {
  if (!props.imageId) return
  try {
    // On utilise /metadata pour avoir les mots-clés (Besoin 11)
    const res = await axios.get(`http://localhost:8080/images/${props.imageId}/metadata`)
    imageDetails.value = res.data
  } catch (err) {
    console.error("Erreur détails", err)
    imageDetails.value = null
  }
}

// Besoin 14 : Charger tous les mots-clés du serveur pour le menu déroulant
const fetchAllGlobalKeywords = async () => {
  try {
    const res = await axios.get('http://localhost:8080/images/keywords')
    existingTags.value = res.data
  } catch (err) {
    console.error("Erreur mots-clés globaux", err)
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


const addKeyword = async (tagFromSelect?: string) => {
  const tagToAdd = tagFromSelect || newTag.value.trim();
  if (!tagToAdd || !props.imageId) return;

  try {
    // PUT /images/{id}/keywords?tag=TAG (Besoin 12)
    await axios.put(`http://localhost:8080/images/${props.imageId}/keywords`, null, {
      params: { tag: tagToAdd }
    });
    newTag.value = "";
    fetchDetails(); // Rafraîchir les mots-clés de l'image
    fetchAllGlobalKeywords(); // Rafraîchir la liste globale
  } catch (err) {
    alert("Erreur lors de l'ajout du mot-clé");
  }
};

const removeKeyword = async (tag: string) => {
  if (!props.imageId) return;
  try {
    // DELETE /images/{id}/keywords?tag=TAG (Besoin 13)
    await axios.delete(`http://localhost:8080/images/${props.imageId}/keywords`, {
      params: { tag: tag }
    });
    fetchDetails();
  } catch (err) {
    alert("Erreur lors de la suppression");
  }
};

// --- AUTRES ACTIONS ---

const downloadImage = async () => {
  if (!props.imageId || !imageDetails.value) return
  try {
    const response = await axios.get(`http://localhost:8080/images/${props.imageId}`, {
      responseType: 'blob' 
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', imageDetails.value.name || `image_${props.imageId}`)
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
  if (!confirm("Supprimer définitivement cette image ?")) return;
  try {
    await axios.delete(`http://localhost:8080/images/${props.imageId}`);
    alert("Image supprimée !");
    emit('image-deleted'); 
  } catch (err) {
    alert("Impossible de supprimer l'image.");
  }
};

watch(() => props.imageId, () => {
  fetchDetails()
  fetchSimilar()
  fetchAllGlobalKeywords()
})

watch(() => [descriptor.value, numSimilar.value], fetchSimilar)

onMounted(() => {
  fetchAllGlobalKeywords()
})

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
      <h4>Informations & Mots-clés</h4>
      <ul class="info-list">
        <li><strong>Nom :</strong> {{ imageDetails.name || imageDetails.Name }}</li>
        <li><strong>Format :</strong> {{ imageDetails.type || imageDetails.Type }}</li>
        <li><strong>Taille :</strong> {{ imageDetails.size || imageDetails.Size }}</li>
      </ul>

      <div class="tags-container">
        <span v-for="tag in imageDetails.keywords || imageDetails.Keywords" :key="tag" class="tag-badge">
          {{ tag }}
          <button @click="removeKeyword(tag)" class="btn-remove-tag" title="Supprimer ce mot-clé">×</button>
        </span>
      </div>

      <div class="add-tag-box">
        <input v-model="newTag" placeholder="Nouveau mot-clé..." @keyup.enter="addKeyword()" />
        
        <select @change="addKeyword(($event.target as HTMLSelectElement).value)">
          <option value="">-- Mots-clés existants --</option>
          <option v-for="t in existingTags" :key="t" :value="t">{{ t }}</option>
        </select>
        
        <button @click="addKeyword()" class="btn-add">Ajouter</button>
      </div>
    </div>

    <h4>Images similaires :</h4>
    <div class="similar-grid">
      <div v-for="sim in similarImages" :key="sim.id" class="sim-card" @click="emit('select-image', sim.id)">
        <img :src="'http://localhost:8080/images/' + sim.id" />
        <span class="score">Score : {{ normalizeScore(sim.score) }}</span>     
      </div>
    </div>

    <div class="actions-bar">
        <button @click="downloadImage" class="btn-save">Sauvegarder</button>
        <button @click="deleteImage" class="btn-delete">Supprimer l'image</button>
    </div>
  </div>
</template>

<style scoped>
    .details-panel { padding: 20px; border: 1px solid #ddd; border-radius: 8px; background: #fff; }
    .preview-big { max-width: 100%; max-height: 400px; display: block; margin: 0 auto 20px; border: 3px solid #42b983; }
    
    .info-list { list-style: none; padding: 0; margin-bottom: 15px; }
    
    /* Tags / Mots-clés */
    .tags-container { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 15px; }
    .tag-badge { 
      background: #42b983; color: white; padding: 4px 10px; 
      border-radius: 15px; font-size: 0.85rem; display: flex; align-items: center; 
    }
    .btn-remove-tag { 
      background: none; border: none; color: white; font-weight: bold; 
      cursor: pointer; margin-left: 8px; font-size: 1.1rem; line-height: 1;
    }
    .btn-remove-tag:hover { color: #2c3e50; }

    .add-tag-box { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
    .add-tag-box input, .add-tag-box select { padding: 6px; border-radius: 4px; border: 1px solid #ccc; flex: 1; min-width: 120px; }
    .btn-add { background: #42b983; color: white; border: none; padding: 6px 15px; border-radius: 4px; cursor: pointer; }

    .similar-grid { display: flex; gap: 15px; flex-wrap: wrap; margin-bottom: 20px; }
    .sim-card { display: flex; flex-direction: column; width: 110px; text-align: center; cursor: pointer; transition: opacity 0.2s; }
    .sim-card:hover { opacity: 0.8; }
    .sim-card img { width: 110px; height: 110px; object-fit: cover; border-radius: 6px; border: 1px solid #eee; }
    .score { font-size: 0.7rem; color: #777; margin-top: 5px; }

    .actions-bar { display: flex; gap: 10px; border-top: 1px solid #eee; padding-top: 20px; }
    .btn-save { background-color: #2c3e50; color: white; border: none; padding: 12px; cursor: pointer; border-radius: 4px; flex: 1; font-weight: bold; }
    .btn-delete { background-color: #e74c3c; color: white; border: none; padding: 12px; cursor: pointer; border-radius: 4px; flex: 1; font-weight: bold; }
    .btn-delete:hover { background-color: #c0392b; }
</style>