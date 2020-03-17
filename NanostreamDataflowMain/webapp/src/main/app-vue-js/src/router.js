import Vue from "vue";
import Router from "vue-router";
import AppHeader from "./components/AppHeader.vue";
import AppFooter from "./components/AppFooter.vue";
// import Components from "./views/Components.vue";
// import Login from "./views/Login.vue";
// import Register from "./views/Register.vue";
// import Profile from "./views/Profile.vue";

// import Vis from "./views/Vis.vue";
import PipelineList from "./views/v1/PipelineList.vue";
import JobList from "./views/v1/JobList.vue";
import Home from "./views/v1/Home.vue";
import NewPipeline from "./views/v1/NewPipeline.vue";
import JobDetails from "./views/v1/JobDetails.vue";
import PipelineDetails from "./views/v1/PipelineDetails.vue";



Vue.use(Router);

export default new Router({
  linkExactActiveClass: "active",
  routes: [
      // {
      //     path: "/",
      //     name: "home",
      //     components: {
      //         header: AppHeader,
      //         default: Home,
      //         // footer: AppFooter
      //     }
      // },

      { path: '/', redirect: '/list' },

      {
          path: "/list",
          name: "pipeline_list",
          props: true,
          components: {
            header: AppHeader,
            default: PipelineList,
            // footer: AppFooter
          }
      },

      {
          path: "/jobs",
          name: "job_list",
          props: true,
          components: {
            header: AppHeader,
            default: JobList,
            // footer: AppFooter
          }
      },

      {
          path: "/new",
          name: "pipeline_new",
          props: true,
          components: {
            header: AppHeader,
            default: NewPipeline,
            // footer: AppFooter
          }
      },

      {
          path: "/job/:job_id/location/:location",
          name: "job_details",
          props: true,
          components: {
            header: AppHeader,
            default: JobDetails,
            // footer: AppFooter
          }
      },

      {
          path: "/pipeline/:pipeline_id",
          name: "pipeline_details",
          props: true,
          components: {
            header: AppHeader,
            default: PipelineDetails,
            // footer: AppFooter
          }
      },


    // {
    //   path: "/login",
    //   name: "login",
    //   components: {
    //     header: AppHeader,
    //     default: Login,
    //     footer: AppFooter
    //   }
    // }
    
  ]
});
