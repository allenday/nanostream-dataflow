import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import Argon from "./plugins/argon-kit";
import BootstrapVue from 'bootstrap-vue'

//import './registerServiceWorker'
//import {Select, Option} from 'element-ui'

Vue.config.productionTip = false;
//Vue.use(Argon);
//Vue.use(Select);
//Vue.use(Option);


Vue.use(BootstrapVue)

new Vue({
  router,
  render: h => h(App)
}).$mount("#app");
