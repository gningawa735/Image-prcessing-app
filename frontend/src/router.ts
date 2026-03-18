import { createWebHistory, createRouter } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/",
    name: "home",
    component: () => import("./components/Home.vue"),
    props: true
  },

  // route pour deposer une image
  {
    path: "/upload",
    name: "upload",
    component: () => import("./components/Upload.vue")
  },

  // route pour afficher la galerie
  {
    path: "/gallery",
    name: "gallery",
    component: () => import("./components/Gallery.vue")
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;