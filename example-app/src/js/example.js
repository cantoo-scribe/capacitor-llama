import { CapacitorLlama } from 'capacitor-llama';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    CapacitorLlama.echo({ value: inputValue })
}
