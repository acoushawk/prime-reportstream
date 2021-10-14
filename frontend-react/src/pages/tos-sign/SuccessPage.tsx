import React from "react";
import Title from "../../components/Title";

const classNames = "usa-prose margin-bottom-4"

const NumberCircle = ({ number, filled, className }: { number: number, filled?: boolean, className?: string }) => {
    const cssStyles: React.CSSProperties = {
        borderRadius: "50%",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        background: filled ? "#000" : "#fff",
        color: filled ? "#fff" : "#000",
        height: '30px',
        width: '30px',
        border: '2px solid black'
    }

    return (
        <div className={className || ""} style={cssStyles}>{number}</div>
    )
}

const Step = ({ number, label, complete }: { number: number, label: string, complete: boolean }) => {
    return (
        <div className="display-flex margin-top-4">
            <NumberCircle className="flex-align-self-center" number={number} filled={complete} />
            <span className="flex-align-self-center margin-left-3">{label}</span>
        </div>
    )
}

function SuccessPage() {
    const steps = [
        {
            number: 1,
            label: "Register to upload",
            complete: true
        },
        {
            number: 2,
            label: "Confirm identity with jurisdiction name",
            complete: false
        },
        {
            number: 3,
            label: "Log in with credentials",
            complete: false
        },
        {
            number: 4,
            label: "Start submitting data",
            complete: false
        },
    ]
    return (
        <div className="width-tablet margin-x-auto">
            <Title preTitle="Account registration" title={`You're almost there, ${"Jane"}!`} />
            <p className={classNames}>
                Our team will reach out to you within one week with credentials to log into ReportStream.
            </p>
            <p className={classNames}>
                A copy of this confirmation has been sent to  [email@address]. If you don't receive confirmation,
                check your SPAM folder for an email from reportstream@cdc.gov.
            </p>
            <p className={classNames}>
                {"Jane"} {"Doe"}
                <br />
                {"Email"}
                <br />
                {"Phone"}
            </p>
            <p className={classNames}>
                {"Jurisdiction 0001"}
                <br />
                {"Organization A"}
                <br />
                {"CLIA: ###"}
            </p>
            <h3 className="padding-top-7 margin-top-7 margin-bottom-7 text-normal border-top-05 border-base-lighter">
                Next steps
            </h3>
            {
                steps.map(step => {
                    return <Step number={step.number} label={step.label} complete={step.complete} />
                })
            }
        </div>
    );
}

export default SuccessPage;
