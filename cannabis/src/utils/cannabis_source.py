class CannabisExampleMetaData:

    def __init__(self, data_line):
        data_list = data_line.split(',')
        self.project = data_list[0]
        self.project_id = data_list[1]
        self.run = data_list[4]
        self.sra_sample = data_list[3]
        self.library_layout = data_list[6]

    def is_paired(self):
        return self.library_layout == "PAIRED"


class CannabisExample:

    def __init__(self, cannabis_example_meta_data, index):
        self.cannabis_example_meta_data = cannabis_example_meta_data
        self.index = index


    def generate_file_name(self):
        run_name = self.cannabis_example_meta_data.run + "_" + str(self.index) + '.fastq'

        if self.cannabis_example_meta_data.project.lower() == "Kannapedia".lower():
            return "kannapedia/" + run_name
        else:
            return "sra/" + self.cannabis_example_meta_data.project_id + "/" \
                   + self.cannabis_example_meta_data.sra_sample + "/" + run_name
