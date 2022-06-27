/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
export default function isObject(input) {
    return Object.prototype.toString.call(input) === '[object Object]';
}
