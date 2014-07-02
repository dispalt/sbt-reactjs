/*global process, require */

(function () {

    "use strict";

    var args = process.argv,
        fs = require("fs"),
        mkdirp = require("mkdirp"),
        path = require("path"),
        jsx =  require('react-tools');

    var SOURCE_FILE_MAPPINGS_ARG = 2;
    var TARGET_ARG = 3;
    var OPTIONS_ARG = 4;

    var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
    var target = args[TARGET_ARG];
    var options = JSON.parse(args[OPTIONS_ARG]);

    var sourcesToProcess = sourceFileMappings.length;
    var results = [];
    var problems = [];

    function compileDone() {
        if (--sourcesToProcess === 0) {
            console.log("\u0010" + JSON.stringify({results: results, problems: problems}));
        }
    }

    function throwIfErr(e) {
        if (e) throw e;
    }

    function endsWith(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    }

    sourceFileMappings.forEach(function (sourceFileMapping) {

        var input = sourceFileMapping[0];
        var outputFile = sourceFileMapping[1].replace(".jsx", ".js");
        var output = path.join(target, outputFile);

        fs.readFile(input, "utf8", function (e, contents) {
            throwIfErr(e);

            try {
                var compileResult = jsx.transform(contents, options);

                mkdirp(path.dirname(output), function (e) {
                    throwIfErr(e);

                    var js = compileResult.code;
                    if (js === undefined) {
                        js = compileResult;
                    }

                    fs.writeFile(output, js, "utf8", function (e) {
                        throwIfErr(e);

                        results.push({
                            source: input,
                            result: {
                                filesRead: [input],
                                filesWritten: [output]
                            }
                        });

                        compileDone();
                    });
                });

            } catch (err) {
                problems.push({
                    message: err.message,
                    severity: "error",
                    lineNumber: err.lineNumber,
                    characterOffset: err.column - 1,
                    lineContent: contents.split("\n")[err.lineNumber - 1],
                    source: input
                });
                results.push({
                    source: input,
                    result: null
                });

                compileDone();
            }
        });
    });
})();
