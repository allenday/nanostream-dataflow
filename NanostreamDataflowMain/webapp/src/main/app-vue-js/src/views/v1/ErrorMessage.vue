<template>
    <div>
        <modal :show.sync="errMsg.show">
            <h6 slot="header" class="modal-title" id="modal-title-default">Error</h6>

            <p>{{ errMsg.message }}</p>

            <template slot="footer">
                <base-button type="primary" class="ml-auto" @click="errMsg.show = false">Close
                </base-button>
            </template>
        </modal>
    </div>
</template>
<script>
    import Modal from "../../components/Modal.vue";
    import BaseButton from "../../components/BaseButton.vue";

    export default {
        name: 'ErrorMessage',

        props: ["errMsg"],

        components: {
            Modal: Modal,
            BaseButton: BaseButton,
        },
    };
</script>
<style>
</style>
