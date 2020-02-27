<template>
    <div class="panel panel-default">
        <div class="panel-body">
            <vue-simple-markdown :source="source"></vue-simple-markdown>
        </div>
    </div>
</template>
<script>
    export default {
        data() {
            return {
                source: "## TODO: add a description here\n" +
                "" +
                "##Nanostream management application " +
                "\n" +
                "" +
                "- Nanostream management application is designed to run/stop previously deployed dataflow templates.\n" +
                "\n" +
                "You can access it by url: ```_ https://<your project id>.appspot.com ```\n\n" +
                "## Reference database structure\n" +
                "You should upload reference database files to your reference database bucket created by install script: \n" +
                "\n" +
                "```\n" +
                "gs://<your project id>-reference-db/\n" +
                "|- reference-sequences/\n" +
                "|-- antibiotic-resistance-genes/\n" +
                "|--- DB.fasta\n" +
                "|--- DB.fasta.[amb,ann,bwt,pac,sa]\n" +
                "|-- species/\n" +
                "|--- DB.fasta\n" +
                "|--- DB.fasta.[amb,ann,bwt,pac,sa]\n" +
                "```\n" +
                "For resistance genes mode you need resistance genes list too:  \n" +
                "```_ gs://<your project id>-reference-db/gene_info/resistance_genes_list.txt```\n"
            }
        }
    };
</script>
