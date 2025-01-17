import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";

import site from "../../../content/site.json";

/* eslint-disable jsx-a11y/anchor-has-content */
export const CsvUploadGuide = () => {
    return (
        <>
            <Helmet>
                <title>
                    CSV upload guide | Organizations and testing facilities |
                    Getting started | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <span className="text-base text-italic">
                    Updated: January 2022
                </span>
                <h2 className=" margin-top-0">
                    CSV upload guide{" "}
                    <span className="text-secondary bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">
                        Pilot program{" "}
                    </span>
                </h2>
                <p className="usa-intro text-base margin-bottom-4">
                    Step-by-step instructions and guidance for preparing and
                    uploading COVID-19 test results via a comma-separated values
                    (CSV) file.
                </p>
                <p>
                    Use a simple online tool to submit a CSV file with a{" "}
                    <a
                        href="/getting-started/testing-facilities/csv-schema"
                        className="usa-link"
                    >
                        standard schema
                    </a>
                    . Receive real-time validation and feedback on file format
                    and field values before submission.
                </p>
                <div className="usa-alert usa-alert--info margin-y-6">
                    <div className="usa-alert__body">
                        <h3 className="usa-alert__heading font-body-md margin-top-05">
                            About CSV upload
                        </h3>
                        This feature is currently being piloted in select
                        jurisdictions with organizations or facilities that have
                        existing Electronic Medical Record (EMR) systems. Pilot
                        partners are selected by recommendation from
                        jurisdictions. Find out if your jurisdiction is{" "}
                        <a
                            href="/how-it-works/where-were-live"
                            className="usa-link"
                        >
                            partnered
                        </a>{" "}
                        with ReportStream and{" "}
                        <a
                            href={
                                "mailto:" +
                                DOMPurify.sanitize(site.orgs.RS.email) +
                                "?subject=Getting started with ReportStream"
                            }
                            className="usa-link"
                        >
                            contact us
                        </a>{" "}
                        to learn more.
                    </div>
                </div>
                <p>
                    <strong>In this guide</strong>
                </p>
                <ul>
                    <li>
                        <a href="#preparing-a-csv" className="usa-link">
                            How to prepare a CSV file for ReportStream
                        </a>
                    </li>
                    <li>
                        <a href="#upload-a-csv" className="usa-link">
                            How to upload a CSV file to ReportStream
                        </a>
                    </li>
                </ul>
                <p>
                    <strong>Resources</strong>
                </p>
                <ul>
                    <li>
                        <a
                            href="/assets/csv/ReportStream-StandardCSV-ExampleData-20220218.csv"
                            className="usa-link"
                        >
                            ReportStream standard CSV with example data
                        </a>
                    </li>
                </ul>
            </section>
            <section>
                <h3
                    id="preparing-a-csv"
                    className="font-body-lg border-top-1px border-ink margin-top-8 margin-bottom-6 padding-top-1"
                >
                    Preparing a CSV file
                </h3>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Download the ReportStream reference CSV and review
                            the documentation
                        </h4>
                        <p className="margin-top-05">
                            If your jurisdiction already has a set format for
                            CSV, compare it to our{" "}
                            <a
                                href="/assets/csv/ReportStream-StandardCSV-ExampleData-20220218.csv"
                                className="usa-link"
                            >
                                standard CSV file
                            </a>{" "}
                            and{" "}
                            <a href="/getting-started/testing-facilities/csv-schema">
                                {" "}
                                CSV schema documentation
                            </a>
                            .
                        </p>
                        <p>
                            If you're starting from scratch, use the standard
                            CSV as a template.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Format your column headers to match the ReportStream
                            standard CSV
                        </h4>
                        <p>
                            Whether you're modifying an existing file or
                            creating a new one from scratch, be sure to include
                            all column headers outlined in the standard file and
                            documentation. Do not include any additional column
                            headers. Make sure headers are written exactly as
                            defined.
                        </p>
                        <p>
                            Include column headers even if you don’t have data
                            for every field.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Enter values into your CSV file
                        </h4>
                        <p>
                            Using the{" "}
                            <a href="/getting-started/testing-facilities/csv-schema">
                                {" "}
                                CSV schema documentation
                            </a>{" "}
                            as a guide, enter properly formatted values in the
                            relevant fields.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Export your CSV
                        </h4>
                        <p>
                            Export your properly formatted CSV with filled-in
                            data.
                        </p>
                        <p>
                            <em>
                                Be sure to save your file as .CSV. ReportStream
                                CSV upload does not accept files saved as .XLS,
                                .XLXS, or other formats.
                            </em>
                        </p>
                    </li>
                </ol>
            </section>
            <section>
                <h3
                    id="upload-a-csv"
                    className="font-body-lg border-top-1px border-ink margin-top-8 margin-bottom-6 padding-top-1"
                >
                    Uploading a CSV file
                </h3>
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Log in to ReportStream
                        </h4>
                        <ul className="margin-top-2">
                            <li>
                                Go to{" "}
                                <a href="/login">
                                    https://reportstream.cdc.gov/login
                                </a>
                            </li>
                            <li>Enter username</li>
                            <li>Enter password</li>
                            <li>Click "Sign in"</li>
                        </ul>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Navigate to "Upload"
                        </h4>
                        <p className="margin-top-2">
                            "Upload" can be found in the main site navigation at
                            the top of the page.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Select file to upload
                        </h4>
                        <ul className="margin-top-2">
                            <li>
                                <strong>Option A:</strong> Drag your CSV file
                                from your folder to the upload area.
                            </li>
                            <li>
                                <strong>Option B:</strong> Click “choose from
                                folder” to browse your computer, select CSV
                                file and click Open.
                            </li>
                        </ul>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Upload the file
                        </h4>
                        <ul className="margin-top-2">
                            <li>Click "Upload."</li>
                            <li>
                                ReportStream will validate your file and provide
                                confirmation that it has been accepted.
                            </li>
                        </ul>
                        <p>
                            <strong>
                                Optional: If you receive errors after clicking
                                "Upload"
                            </strong>
                        </p>
                        <p>
                            ReportStream validates all uploaded files against a{" "}
                            <a
                                href="/getting-started/testing-facilities/csv-schema"
                                className="usa-link"
                            >
                                standard CSV schema
                            </a>
                            . If any errors are detected with a file's format or
                            data, the application will alert you to specific
                            changes you need to make before the file can be
                            submitted.
                        </p>
                        <p>Resolving errors with a CSV:</p>
                        <ul>
                            <li>
                                Review the list of errors and recommended
                                changes suggested by ReportStream. Reference the{" "}
                                <a
                                    href="/getting-started/testing-facilities/csv-schema"
                                    className="usa-link"
                                >
                                    CSV schema documentation
                                </a>{" "}
                                again for any adjustments to column headers or
                                values.
                            </li>
                            <li>
                                Make the recommended changes to the file and
                                re-export it as a .CSV
                            </li>
                            <li>
                                Return to step 3 and upload the corrected file
                            </li>
                        </ul>
                    </li>
                </ol>
            </section>
        </>
    );
};
