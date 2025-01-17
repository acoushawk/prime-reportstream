export type SubmissionDate = {
    dateString: string;
    timeString: string;
};

/* 
    This function serves as a cleaner (read: more contained) way of parsing out
    necessary date and time string formats for the Submissions details page.

    @param dateTimeString - the value representing when a report was sent, returned
    by the API  
    
    @returns SubmissionDate | null
    dateString format: 1 Jan 2022
    timeString format: 16:30
*/
export const generateDateTitles = (
    dateTimeString: string
): SubmissionDate | null => {
    const dateRegex = /\d{1,2} [a-z,A-Z]{3} \d{4}/;
    const timeRegex = /\d{1,2}:\d{2}/;

    const date = new Date(dateTimeString);
    const monthString = parseMonth(date.getMonth());

    const dateString = `${date.getDate()} ${monthString} ${date.getFullYear()}`;
    const timeString = `${date.getHours()}:${date.getMinutes()}`;

    if (!dateString.match(dateRegex) || !timeString.match(timeRegex))
        return null;

    return {
        dateString: dateString,
        timeString: timeString,
    };
};

const parseMonth = (numericMonth: number) => {
    const monthNames = [
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
    ];
    return monthNames[numericMonth];
};
