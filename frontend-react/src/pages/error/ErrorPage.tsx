// @ts-nocheck // TODO: fix types in this file
import { Alert } from "@trussworks/react-uswds";
import React from "react";
import Helmet from "react-helmet";

import site from "../../content/site.json";

import { NotFound } from "./NotFound";
import { UnsupportedBrowser } from "./UnsupportedBrowser";

interface ErrorPageProps {
    code?: string;
    error?: string;
    errorInfo?: React.ErrorInfo;
    type?: "page" | "message";
}

/* INFO
   For consistency, when passing the code prop, please use these values
   e.g. <ErrorPage code={CODES.NOT_FOUND_404} />
 */
export enum CODES {
    UNSUPPORTED_BROWSER = "unsupported-browser",
    NOT_FOUND_404 = "not-found",
}

function ErrorPageWrapper({ children }: JSX.Element) {
    return (
        <div className="usa-section padding-top-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">{children}</div>
            </div>
        </div>
    );
}

function ErrorMessageWrapper({ children }: JSX.Element) {
    return (
        <div className="grid-container">
            <div className="tablet:grid-col-8">{children}</div>
        </div>
    );
}

function GenericErrorMessage(): JSX.Element {
    return (
        <Alert type="error">
            <h2 className="usa-alert__heading">Error: not found</h2>
            <p className="usa-alert__text">
                There was an error loading this content. If you continue to experience this error, contact us at reportstream@cdc.gov.
            </p>
        </Alert>
    );
}

function GenericErrorPage(): JSX.Element {
    return (
        <>
            <Helmet>
                <title>Error</title>
            </Helmet>
            <div className="usa-prose">
                <h1>An error has occurred</h1>
                <p className="usa-intro">The application has encountered an unknown error.</p>
                <p>
                    This event has been logged and our technical staff have been automatically notified.
                </p>
                <div className="margin-y-5">
                    <ul className="usa-button-group">
                        <li className="usa-button-group__item">
                            <a href="./" className="usa-button">
                                Visit homepage
                            </a>
                        </li>
                        <li className="usa-button-group__item">
                            <a
                                className="usa-button usa-button--outline"
                                href={"mailto:" + site.orgs.RS.email}
                            >
                                Contact us
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </>
    );
}

export function ErrorPage(props: ErrorPageProps) {
    const codes = {
        "not-found": <NotFound />,
        "unsupported-browser": <UnsupportedBrowser />,
    };
    const content = codes[props.code];

    if (content) {
        return <ErrorPageWrapper>{content}</ErrorPageWrapper>;
    }
    if (props.type === "message") {
        return (
            <ErrorMessageWrapper>
                <GenericErrorMessage />
            </ErrorMessageWrapper>
        );
    } else {
        return (
            <ErrorPageWrapper>
                <GenericErrorPage />
            </ErrorPageWrapper>
        );
    }
}

ErrorPage.defaultProps = {
    type: "message",
};
