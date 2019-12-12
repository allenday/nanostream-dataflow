import Vue from "vue";
import Router from "vue-router";
import AppHeader from "./components/AppHeader";
import AppFooter from "./components/AppFooter";
import Components from "./views/Components.vue";
import Login from "./views/Login.vue";
import Register from "./views/Register.vue";
import Profile from "./views/Profile.vue";

import Vis from "./views/Vis.vue";



Vue.use(Router);

export default new Router({
  linkExactActiveClass: "active",
  routes: [
    {
      path: "/",
      name: "components",
      components: {
        header: AppHeader,
        default: Vis,
        footer: AppFooter
      }
    },

    {
      // path: "/vis/c/:c/d/:d/m/:m",
      path: "/vis",
      name: "vis",
      props: true,
      components: {
        header: AppHeader,
        default: Vis,
        footer: AppFooter
      }
    },


    {
      path: "/login",
      name: "login",
      components: {
        header: AppHeader,
        default: Login,
        footer: AppFooter
      }
    }
    
  ]
});
