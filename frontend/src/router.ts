import { createWebHistory, createRouter } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/",
    name: "home",
    component: () => import("./components/Home.vue"),
    props: true,
  },
  {
    path: "/upload",
    name: "upload",
    component: () => import("./components/Upload.vue"),
  },
  {
    path: "/gallery",
    name: "gallery",
    component: () => import("./components/Gallery.vue"),
  },
  {
    path: "/search",
    name: "search",
    component: () => import("./components/Search.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;