python_versions_base = ['3.5', '3.6', '3.7', '3.8', '3.9', '3.10', '3.11', '3.12', '3.13']

python_versions_underscore = [x.replace('.', '_') for x in python_versions_base]

grammar_parser_map = {
    '3_5': 'PythonGrammar30',
    '3_6': 'PythonGrammar36',
    '3_7': 'PythonGrammar36',
    '3_8': 'PythonGrammar38',
    '3_9': 'PythonGrammar38',
    '3_10': 'PythonGrammar310',
    '3_11': 'PythonGrammar311',
    '3_12': 'PythonGrammar312',
    '3_13': 'PythonGrammar312',
}
