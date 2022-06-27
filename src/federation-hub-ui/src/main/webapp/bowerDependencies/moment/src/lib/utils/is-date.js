/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
export default function isDate(input) {
    return input instanceof Date || Object.prototype.toString.call(input) === '[object Date]';
}
