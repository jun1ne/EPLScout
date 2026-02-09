const commonChartOptions = {
    responsive: true,
    plugins: {
        legend: {
            position: "top",
            labels: {
                boxWidth: 14,
                font: { size: 12 }
            }
        },
        tooltip: {
            callbacks: {
                label: ctx =>
                    `${ctx.dataset.label}: ${ctx.formattedValue}`
            }
        }
    },
    scales: {
        x: {
            ticks: {
                maxRotation: 45,
                minRotation: 30,
                font: { size: 11 }
            }
        },
        y: {
            ticks: {
                font: { size: 11 }
            }
        }
    }
};
