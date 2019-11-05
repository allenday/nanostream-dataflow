<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <link href='styles.css' rel='stylesheet' type='text/css'>
  <title>Nanostream</title>
</head>
<body>
    <h1>Define the following variables to run the pipeline</h1>
    <form action='/launch' method='post'>
        <div>
            <label for="pipeline_name">Pipeline name</label>
            <input type="text" id="pipeline_name" name="pipeline_name"  title="This is the text of the tooltip"  />
        </div>
        <div>
            <label for="document_name">Document name</label>
            <input type="text" id="document_name" name="document_name" />
        </div>
        <div>
            <label>Reference database</label>
            <div>
                <label for="reference_database_predefined">Use predefined:</label>
                <input type="radio" name="reference-database" id="reference_database_predefined" checked>
                <select name="reference_database_predefined_name" autofocus="true">
                    <option value="species">Species</option>
                    <option value="resistance_genes">Resistance genes</option>
                </select>
            </div>
            <div>
                <label for="reference_database_custom">Custom:</label>
                <input type="radio" name="reference-database" id="reference_database_custom">
                <input type="text" id="reference_database_custom1" />
            </div>
            <div>
                <label for="alignment_window">Alignment window</label>
                <input type="text" id="alignment_window" name="alignment_window" title="the size of the window (in wall-clock seconds) in which FastQ records will be collected for alignment" />
            </div>
        </div>
        <div>
            <input type="submit" value='Start'/>
        </div>
    </form>
</body>
</html>
