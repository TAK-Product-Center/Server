var tags;
var latest;
var currentTag;
var definitionPath;

const TAK_S3_PATH = getTakDocPath();
const TAGS_FILE = `${TAK_S3_PATH}/tags.json`;
const marginStyle = "margin: 0em 0.5em;";
// successful green from ATAK Arsenal Design Library
const copySuccessStyle = "border-color: #0EA900;color: #0EA900;";
const versionLabelText = "Version:";
const copyLinkButtonText = "Copy Version Link";
const successText = "Version Link Copied to Clipboard";

var isDefined = (e) => { return typeof e !== 'undefined'; }
var defineOasPath = (tag) => { return `${TAK_S3_PATH}/${tag}/openapispec.json`; }
var getShareUrl = (tag = currentTag) => { return `${TAK_S3_PATH}?takVersion=${tag}`; }

function getTakDocPath() {
    let currentLocation = window.location.origin + window.location.pathname;
    // Remove "/redoc" from the pathname
    currentLocation = currentLocation.replace(/\/redoc$/, '');
    return currentLocation;
}

function getQueryParams() {
    let queryParams = {};
    let query = window.location.search.substring(1);
    let match;
    let search = /([^&=]+)=?([^&]*)/g;
    while (match = search.exec(query)) {
        queryParams[decodeURIComponent(match[1])] = decodeURIComponent(match[2]);
    }
    return queryParams;
}

function checkQueryTag(defaultTag) {
    const queryTag = getQueryParams().takVersion;
    return isDefined(queryTag) ? queryTag : defaultTag;
}

async function getTags(tags = undefined, update = false) {
    if (isDefined(tags) && !update) return { latest, tags };
    return await fetch(TAGS_FILE).then((res) => { return res.json(); })
        .then((tags) => {
            latest = tags[0];
            return { latest, tags };
        });
}

function changeButtonStyle(copyLinkButton, style, text) {
    copyLinkButton.setAttribute("style", style);
    copyLinkButton.textContent = text;
}

function createCopyLinkButton() {
    // Create button with same class as redoc's download anchor
    const anchors = document.getElementsByTagName("a")
    const downloadClass = anchors[1].getAttribute("class");
    
    const copyLinkButton = document.createElement("button");
    copyLinkButton.setAttribute("class", downloadClass);

    const setRegularButtonStyle = () => changeButtonStyle(copyLinkButton, marginStyle, copyLinkButtonText);
    setRegularButtonStyle();

    shareUrl = getShareUrl();
    copyLinkButton.title = shareUrl;
    // Copy shareable URL to clipboard, temporarily change style to acknowledge successful click
    copyLinkButton.onclick = () => {
        navigator.clipboard.writeText(shareUrl);
        changeButtonStyle(copyLinkButton, marginStyle + copySuccessStyle, successText);
        setTimeout(setRegularButtonStyle, 3000)
    }
    return copyLinkButton;
}

function createVersionSelect() {
    const versionSelect = document.createElement("select");
    const groupedTags = groupTagsByMajorVersion(tags);

    for (const majorVersion in groupedTags) {
        const optgroup = document.createElement("optgroup");
        optgroup.label = majorVersion;

        groupedTags[majorVersion].forEach(tag => {
            const option = document.createElement("option");
            option.value = tag;
            option.textContent = tag;
            if (tag == currentTag) {
                option.setAttribute("selected", "selected");
            }
            optgroup.appendChild(option);
        });

        versionSelect.appendChild(optgroup);
    }

    // On changed version selection, re-initiate redoc with that version's definitionPath
    versionSelect.addEventListener('change', function handleChange(event) {
        currentTag = event.target.value;
        definitionPath = defineOasPath(currentTag);
        renderRedoc(definitionPath);
    });

    return versionSelect;
}

// Helper function to group tags by major version
function groupTagsByMajorVersion(tags) {
    const groupedTags = {};

    tags.forEach(tag => {
        const [majorVersion, releaseType, minorVersion] = tag.split('-');
        const key = `${majorVersion}-${releaseType}`;
        groupedTags[key] = groupedTags[key] || [];
        groupedTags[key].push(tag);
    });

    return groupedTags;
}

function createVersionElements() {
    // Get Redoc's menu-content element to copy its class/style.
    const menuContent = document.getElementsByClassName("menu-content")[0];
    const menuItem = menuContent.children[1].firstChild.firstChild.firstChild;
    const clazz = menuItem.getAttribute("class");
    // TODO: make sure this isn't an active menu item's class (for if the user had the first item selected)
    clazz.replace(" active", "");

    // Create label for drop-down, apply class to match menu-content style
    const selectLabel = document.createElement("label");
    selectLabel.appendChild(document.createTextNode(versionLabelText));
    selectLabel.setAttribute("class", clazz);

    // Create and add drop-down element to the label, insert above search element
    selectLabel.appendChild(createVersionSelect());
    menuContent.insertBefore(selectLabel, menuContent.firstChild);

    // Create and add button to copy shareable URL, insert above search
    menuContent.insertBefore(createCopyLinkButton(), menuContent.children[1])
}

function renderVersionElements() {
    try {
        createVersionElements()
    } catch (e) {
        console.error(e);
    }
}

function renderRedoc(definitionPath) {
    // Add version drop-down and its change listener after redoc elements load
    Redoc.init(
        definitionPath,
        {},
        document.getElementById('redoc-container'),
        renderVersionElements
    );
}

getTags()
    .then((result) => {
        latest = result.latest;
        tags = result.tags;
        // check if url includes query params specifying a tag
        currentTag = checkQueryTag(latest);
        definitionPath = defineOasPath(currentTag)
        renderRedoc(definitionPath);
    });
