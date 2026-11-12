// Captura la salida de mvn verify, el detalle de pruebas, el reporte de JaCoCo y la ejecucion del CI.
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
  const local = (archivo) => 'file://' + path.resolve(archivo);
  const vistas = [
    ['pruebas-en-verde', local('evidencias/salida-mvn-verify.html'), 1100],
    ['pruebas-por-metodo', local('evidencias/pruebas-por-metodo.html'), 1100],
    ['arranque-spring-boot', local('evidencias/arranque.html'), 1400],
    ['cobertura-jacoco', local('target/site/jacoco/index.html'), 1280],
    ['cobertura-jacoco-service', local('target/site/jacoco/com.clinica.recordatorios.service/index.html'), 1280],
  ];
  if (process.env.RUN_ID) {
    vistas.push(['actions-ci-verde',
      `https://github.com/${process.env.REPO}/actions/runs/${process.env.RUN_ID}`, 1440]);
  }
  const navegador = await chromium.launch();
  for (const [nombre, url, ancho] of vistas) {
    if (url.startsWith('file://') && !fs.existsSync(url.slice(7))) {
      console.log('No existe', url);
      continue;
    }
    const pagina = await navegador.newPage({ viewport: { width: ancho, height: 800 }, deviceScaleFactor: 2 });
    await pagina.goto(url, { waitUntil: 'networkidle' });
    await pagina.waitForTimeout(1500);
    await pagina.screenshot({ path: `evidencias/${nombre}.png`, fullPage: true });
    await pagina.close();
  }
  await navegador.close();
})().catch((error) => { console.error(error); process.exit(1); });
