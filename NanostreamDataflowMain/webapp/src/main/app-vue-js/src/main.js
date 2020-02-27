import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import Argon from "./plugins/argon-kit";
import BootstrapVue from 'bootstrap-vue'
import VueSimpleMarkdown from 'vue-simple-markdown'
import Loading from 'vue-loading-overlay';
import 'vue-loading-overlay/dist/vue-loading.css';

//import './registerServiceWorker'
//import {Select, Option} from 'element-ui'
Vue.use(Loading);

// const fetch = window.fetch;
// window.fetch = (...args) => (async(args) => {
//   console.log('before fetch');
//
//   let loader = Vue.$loading.show({
//     // Optional parameters
//     // container: null
//     // canCancel: true,
//     // onCancel: this.onCancel,
//   });
//
//   var result = await fetch(...args);
//   // console.log(result); // intercept response here
//   console.log('after fetch');
//   loader.hide()
//
//   return result;
// })(args);

Vue.config.productionTip = false;
//Vue.use(Argon);
//Vue.use(Select);
//Vue.use(Option);

Vue.use(BootstrapVue);
Vue.use(VueSimpleMarkdown);

Vue.prototype.$vueEventBus = new Vue();  // Global event bus

new Vue({
  router,
  render: h => h(App)
}).$mount("#app");
