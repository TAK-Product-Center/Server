import './MenuBar.css';
import React, { useEffect, useState }from'react';

function MenuBar() {
  let[htmlFileString, setHtmlFileString] = useState();

  async function fetchHtml() {
    const response = await fetch(`/Marti/menubar.html`);
    if (response.ok){
        const html = response.text();
        setHtmlFileString(await html);
        return html;
    }
  }

  function waitForElm(selector) {
    return new Promise(resolve => {
        if (document.querySelector(selector)) {
            return resolve(document.querySelector(selector));
        }

        const observer = new MutationObserver(mutations => {
            if (document.querySelector(selector)) {
                resolve(document.querySelector(selector));
                observer.disconnect();
            }
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    });
   }

  function addAccordian() {
    var acc = document.getElementsByClassName("accordion");
    var i;
    for (i = 0; i < acc.length; i++) {
        acc[i].addEventListener("click", function() {
            /* Toggle between adding and removing the "active" class,
            to highlight the button that controls the panel */
            this.classList.toggle("active");

            /* Toggle between hiding and showing the active panel */
            var panel = this.nextElementSibling;
            if (panel.style.display === "block") {
                panel.style.display = "none";
            } else {
                panel.style.display = "block";
            }
        });
        console.log(acc[i]);
    }
  }

  useEffect(() => {
    fetchHtml().then(html => {
        if (html){
            var extractScript = /<script type='text\/javascript'>([\s\S]*?)<\/script>/im.exec(html);
            if (extractScript){ 
                var script = document.createElement("script");
                script.setAttribute('type','text/javascript');
                script.innerHTML = extractScript[1];
                document.body.appendChild(script);
            }
        };})
    waitForElm('.accordion').then((elm) => {
        addAccordian();
    });
  }, []);

  return(
      <div dangerouslySetInnerHTML={{ __html: htmlFileString }}></div>
  );
}

export default MenuBar;